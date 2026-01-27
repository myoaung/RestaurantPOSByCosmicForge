package com.cosmicforge.pos.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.cosmicforge.pos.core.network.model.ConnectionType
import com.cosmicforge.pos.core.network.model.PeerDevice
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network Service Discovery Manager
 * Provides fallback discovery when devices are connected to the same WiFi router
 * Better range than WiFi Direct
 */
@Singleton
class NSDManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    
    private val _discoveredServices = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredServices: StateFlow<List<PeerDevice>> = _discoveredServices.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()
    
    private val discoveredServicesMap = mutableMapOf<String, NsdServiceInfo>()
    
    /**
     * Register this device as a service
     */
    fun registerService(deviceName: String, port: Int = SERVICE_PORT) {
        if (_isRegistered.value) {
            Log.w(TAG, "Service already registered")
            return
        }
        
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            this.port = port
        }
        
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
                _isRegistered.value = false
            }
            
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode")
            }
            
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${serviceInfo.serviceName}")
                _isRegistered.value = true
            }
            
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                _isRegistered.value = false
            }
        }
        
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering service", e)
        }
    }
    
    /**
     * Start discovering services on the local network
     */
    fun startDiscovery() {
        if (_isDiscovering.value) {
            Log.w(TAG, "Discovery already in progress")
            return
        }
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started")
                _isDiscovering.value = true
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Service discovery stopped")
                _isDiscovering.value = false
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                
                // Resolve the service to get full details
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode for ${serviceInfo.serviceName}")
                    }
                    
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service resolved: ${resolvedInfo.serviceName} at ${resolvedInfo.host}:${resolvedInfo.port}")
                        
                        // Add to discovered services
                        discoveredServicesMap[resolvedInfo.serviceName] = resolvedInfo
                        updateDiscoveredPeers()
                    }
                })
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                discoveredServicesMap.remove(serviceInfo.serviceName)
                updateDiscoveredPeers()
            }
            
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                _isDiscovering.value = false
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }
        }
        
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
        }
    }
    
    /**
     * Stop service discovery
     */
    fun stopDiscovery() {
        if (!_isDiscovering.value) {
            Log.w(TAG, "Discovery not in progress")
            return
        }
        
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }
    
    /**
     * Unregister service
     */
    fun unregisterService() {
        if (!_isRegistered.value) {
            Log.w(TAG, "Service not registered")
            return
        }
        
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }
    }
    
    /**
     * Get service info by name
     */
    fun getServiceInfo(serviceName: String): NsdServiceInfo? {
        return discoveredServicesMap[serviceName]
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up NSD resources")
        stopDiscovery()
        unregisterService()
        discoveredServicesMap.clear()
    }
    
    private fun updateDiscoveredPeers() {
        _discoveredServices.value = discoveredServicesMap.values.map { serviceInfo ->
            PeerDevice(
                deviceName = serviceInfo.serviceName,
                deviceAddress = "${serviceInfo.host.hostAddress}:${serviceInfo.port}",
                connectionType = ConnectionType.NSD_LOCAL,
                isConnected = false // Will be updated when socket connects
            )
        }
    }
    
    companion object {
        private const val TAG = "NSDManager"
        private const val SERVICE_TYPE = "_cosmicforge._tcp."
        private const val SERVICE_PORT = 8888
    }
}
